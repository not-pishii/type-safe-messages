package me.supcheg.messages.processor;

import me.supcheg.javafile.JavaFile;
import me.supcheg.javafile.langmodel.Descriptors;
import me.supcheg.javafile.model.Modifier;
import me.supcheg.javafile.type.ClassTypeRef;
import me.supcheg.javafile.type.ParameterizedTypeRef;
import me.supcheg.javafile.type.TypeVarRef;
import me.supcheg.javafile.type.Types;
import me.supcheg.messages.MessageRenderer;
import me.supcheg.messages.MessageTemplate;
import me.supcheg.messages.load.BundleLoader;
import me.supcheg.messages.load.ContentProblem;
import me.supcheg.messages.spi.TemplateProvider;
import me.supcheg.routine.Either;

import java.lang.constant.ClassDesc;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static me.supcheg.javafile.code.Exprs.field;
import static me.supcheg.javafile.code.Exprs.lambda;
import static me.supcheg.javafile.code.Exprs.literal;
import static me.supcheg.javafile.code.Exprs.new_;
import static me.supcheg.javafile.code.Exprs.staticCall;
import static me.supcheg.javafile.code.Exprs.staticField;
import static me.supcheg.javafile.type.Types.parameterized;

final class RuntimeBundleWriter {

    private static final ClassTypeRef Type_Path = Types.of(Path.class);
    private static final ClassTypeRef Type_Locale = Types.of(Locale.class);
    public static final ClassTypeRef Type_Function = Types.of(Function.class);
    private static final ClassTypeRef Type_Override = Types.of(Override.class);
    private static final ClassTypeRef Type_Either = Types.of(Either.class);

    private static final ClassTypeRef Type_ContentProblem = Types.of(ContentProblem.class);
    private static final ClassTypeRef Type_TemplateProvider = Types.of(TemplateProvider.class);
    private static final ClassTypeRef Type_MessageRenderer = Types.of(MessageRenderer.class);
    private static final ClassTypeRef Type_MessageTemplate = Types.of(MessageTemplate.class);
    private static final ClassTypeRef Type_BundleLoader = Types.of(BundleLoader.class);

    private static final ParameterizedTypeRef Type_List_ContentProblem = parameterized(Types.LIST, Type_ContentProblem);
    private static final TypeVarRef TypeVar_T = Types.typeVar("T");
    private static final ParameterizedTypeRef Type_MessageRenderer_T = parameterized(Type_MessageRenderer, TypeVar_T);
    private static final ParameterizedTypeRef Type_Map_String_MessageTemplate =
            parameterized(Types.MAP, Types.STRING, Type_MessageTemplate);
    private static final ParameterizedTypeRef Type_Function_String_Object =
            parameterized(Type_Function, Types.STRING, Types.OBJECT);

    private RuntimeBundleWriter() {}

    static JavaFile write(BundleModel model) {
        var contractType =
                ClassDesc.of(model.contract().packageName(), model.contract().simpleName());
        var contractMetaType =
                ClassDesc.of(model.contract().packageName(), model.contract().simpleName() + "Contract");
        var selfType = ClassDesc.of(model.packageName(), model.contract().simpleName() + "RuntimeBundle");
        var implType = selfType.nested("Impl");

        var loadType = parameterized(Type_Either, Type_List_ContentProblem, parameterized(contractType, TypeVar_T));
        return JavaFile.class_(
                selfType, cb -> cb.withMethod("load", loadType, mb -> mb.withModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .withTypeParam("T")
                                .withParam("dir", Type_Path)
                                .withParam("locale", Type_Locale)
                                .withParam("renderer", Type_MessageRenderer_T)
                                .withBody(body -> body.return_(staticCall(
                                                Type_BundleLoader,
                                                "load",
                                                field("dir"),
                                                field("locale"),
                                                literal(model.resources()),
                                                staticField(contractMetaType, "SHAPE"))
                                        .call(
                                                "mapRight",
                                                lambda(
                                                        List.of("content"),
                                                        new_(implType, field("content"), field("renderer")))))))
                        .withMethod("load", loadType, mb -> mb.withModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .withTypeParam("T")
                                .withParam("provider", Type_TemplateProvider)
                                .withParam("locale", Type_Locale)
                                .withParam("renderer", Type_MessageRenderer_T)
                                .withBody(body -> body.return_(staticCall(
                                                Type_BundleLoader,
                                                "load",
                                                field("provider"),
                                                field("locale"),
                                                staticField(contractMetaType, "SHAPE"))
                                        .call(
                                                "mapRight",
                                                lambda(
                                                        List.of("content"),
                                                        new_(implType, field("content"), field("renderer")))))))
                        .withNestedRecord(implType, rb -> {
                            rb.withTypeParam("T")
                                    .withComponent("content", Type_Map_String_MessageTemplate)
                                    .withComponent("renderer", Type_MessageRenderer_T)
                                    .withInterface(parameterized(contractType, TypeVar_T));
                            for (var message : model.contract().messages()) {
                                rb.withMethod(message.methodName(), TypeVar_T, mb -> {
                                    mb.withAnnotation(Type_Override.desc()).withModifiers(Modifier.PUBLIC);
                                    for (var param : message.params()) {
                                        mb.withParam(param.name(), Descriptors.toTypeRef(param.type()));
                                    }
                                    mb.withBody(body -> body.localVar(
                                                    "args",
                                                    Type_Function_String_Object,
                                                    MethodSignatures.argumentsFunctionExpr(message))
                                            .return_(field("content")
                                                    .call("get", literal(message.key()))
                                                    .call("render", field("renderer"), field("args"))));
                                });
                            }
                        })
                        .withConstructor(constructorBuilder -> constructorBuilder.withModifiers(Modifier.PRIVATE)));
    }
}
