package me.supcheg.messages.processor;

import me.supcheg.javafile.JavaFile;
import me.supcheg.javafile.langmodel.Descriptors;
import me.supcheg.javafile.model.Modifier;
import me.supcheg.javafile.type.ClassTypeRef;
import me.supcheg.javafile.type.ParameterizedTypeRef;
import me.supcheg.javafile.type.TypeVarRef;
import me.supcheg.javafile.type.Types;
import me.supcheg.messages.Literal;
import me.supcheg.messages.MessageRenderer;
import me.supcheg.messages.MessageTemplate;
import me.supcheg.messages.Placeholder;

import java.lang.constant.ClassDesc;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

import static me.supcheg.javafile.code.Exprs.call;
import static me.supcheg.javafile.code.Exprs.field;
import static me.supcheg.javafile.code.Exprs.literal;
import static me.supcheg.javafile.code.Exprs.new_;
import static me.supcheg.javafile.code.Exprs.staticCall;
import static me.supcheg.javafile.code.Exprs.switchExpr;
import static me.supcheg.javafile.type.Types.parameterized;

final class CompileTimeBundleWriter {

    private static final ClassTypeRef Type_Locale = Types.of(Locale.class);
    private static final ClassTypeRef Type_Function = Types.of(Function.class);
    private static final ClassTypeRef Type_Override = Types.of(Override.class);

    private static final ClassTypeRef Type_MessageTemplate = Types.of(MessageTemplate.class);
    private static final ClassTypeRef Type_MessageRenderer = Types.of(MessageRenderer.class);
    private static final ClassTypeRef Type_Literal = Types.of(Literal.class);
    private static final ClassTypeRef Type_Placeholder = Types.of(Placeholder.class);

    private static final ParameterizedTypeRef Type_Set_Locale = parameterized(Types.SET, Type_Locale);
    private static final ParameterizedTypeRef Type_Function_String_Object =
            parameterized(Type_Function, Types.STRING, Types.OBJECT);

    private static final TypeVarRef TypeVar_T = Types.typeVar("T");
    private static final ParameterizedTypeRef Type_MessageRenderer_T = parameterized(Type_MessageRenderer, TypeVar_T);

    private CompileTimeBundleWriter() {}

    static JavaFile write(BundleModel model, Map<String, Map<String, MessageTemplate>> byLocale) {
        var contractType =
                ClassDesc.of(model.contract().packageName(), model.contract().simpleName());
        var selfType = ClassDesc.of(model.packageName(), model.contract().simpleName() + "Bundle");

        return JavaFile.class_(selfType, cb -> {
            cb.withModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .withField("LOCALES", Type_Set_Locale, fb -> fb.withModifiers(Modifier.PRIVATE, Modifier.STATIC)
                            .withInitializer(staticCall(
                                    Types.SET,
                                    "of",
                                    model.localeTags().stream()
                                            .map(tag -> staticCall(Type_Locale, "forLanguageTag", literal(tag)))
                                            .toList())))
                    .withMethod("locales", Type_Set_Locale, mb -> mb.withModifiers(Modifier.STATIC)
                            .withBody(body -> body.return_(field("LOCALES"))));

            for (var tag : model.localeTags()) {
                var tagImplType = selfType.nested(BundleNaming.className(tag));
                cb.withMethod(
                        BundleNaming.methodName(tag), parameterized(contractType, TypeVar_T), mb -> mb.withTypeParam(
                                        TypeVar_T)
                                .withModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .withParam("renderer", Type_MessageRenderer_T)
                                .withBody(body -> body.return_(new_(tagImplType, field("renderer")))));

                var content = byLocale.get(tag);
                cb.withNestedRecord(tagImplType, rb -> {
                    rb.withTypeParam(TypeVar_T)
                            .withComponent("renderer", Type_MessageRenderer_T)
                            .withInterface(parameterized(contractType, TypeVar_T));
                    for (var message : model.contract().messages()) {
                        var messageConstantName = BundleNaming.constantName(message.methodName());
                        var parts = content.get(message.key()).parts().stream()
                                .map(part -> switch (part) {
                                    case Literal(var text) -> new_(Type_Literal, literal(text));
                                    case Placeholder(var name) -> new_(Type_Placeholder, literal(name));
                                })
                                .toList();

                        rb.withStaticField(messageConstantName, Type_MessageTemplate, fb -> fb.withModifiers(
                                        Modifier.PRIVATE, Modifier.FINAL)
                                .withInitializer(new_(
                                        Type_MessageTemplate,
                                        literal(message.key()),
                                        staticCall(Types.LIST, "of", parts))));
                        rb.withMethod(message.methodName(), TypeVar_T, mb -> {
                            mb.withAnnotation(Type_Override.desc());
                            for (var param : message.params()) {
                                mb.withParam(param.name(), Descriptors.toTypeRef(param.type()));
                            }
                            mb.withBody(body -> body.localVar(
                                            "args",
                                            Type_Function_String_Object,
                                            MethodSignatures.argumentsFunctionExpr(message))
                                    .return_(field(messageConstantName)
                                            .call("render", field("renderer"), field("args"))));
                        });
                    }
                });
            }
            cb.withMethod(
                    "forLocale",
                    parameterized(Types.OPTIONAL, parameterized(contractType, TypeVar_T)),
                    mb -> mb.withModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .withTypeParam(TypeVar_T)
                            .withParam("locale", Type_Locale)
                            .withParam("renderer", Type_MessageRenderer_T)
                            .withBody(body ->
                                    body.return_(switchExpr(field("locale").call("toLanguageTag"), sb -> {
                                        for (var tag : model.localeTags()) {
                                            sb.caseValue(
                                                    literal(tag),
                                                    staticCall(
                                                            Types.OPTIONAL,
                                                            "of",
                                                            call(BundleNaming.methodName(tag), field("renderer"))));
                                        }
                                        sb.defaultValue(staticCall(Types.OPTIONAL, "empty"));
                                    }))));
            cb.withConstructor(constructorBuilder -> constructorBuilder.withModifiers(Modifier.PRIVATE));
        });
    }
}
