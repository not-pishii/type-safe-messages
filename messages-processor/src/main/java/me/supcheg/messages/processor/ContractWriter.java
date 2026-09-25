package me.supcheg.messages.processor;

import me.supcheg.javafile.JavaFile;
import me.supcheg.javafile.annotation.AnnotationBuilder;
import me.supcheg.javafile.annotation.AnnotationUse;
import me.supcheg.javafile.annotation.AnnotationValues;
import me.supcheg.javafile.annotation.SingleAnnotationValue;
import me.supcheg.javafile.code.Expr;
import me.supcheg.javafile.code.Exprs;
import me.supcheg.javafile.langmodel.Descriptors;
import me.supcheg.javafile.model.Modifier;
import me.supcheg.javafile.type.ClassTypeRef;
import me.supcheg.javafile.type.PrimitiveTypeRef;
import me.supcheg.javafile.type.Types;
import me.supcheg.messages.annotation.meta.ContractMeta;
import me.supcheg.messages.annotation.meta.MessageMeta;
import me.supcheg.messages.annotation.meta.ParamMeta;
import me.supcheg.messages.load.ContractShape;
import me.supcheg.messages.load.MessageShape;

import javax.lang.model.type.TypeMirror;
import java.lang.constant.ClassDesc;

import static me.supcheg.javafile.annotation.AnnotationValues.array;
import static me.supcheg.javafile.annotation.AnnotationValues.literal;
import static me.supcheg.javafile.code.Exprs.new_;
import static me.supcheg.javafile.code.Exprs.staticCall;

final class ContractWriter {
    private static final ClassDesc CD_ContractMeta =
            Types.of(ContractMeta.class).desc();
    private static final ClassDesc CD_MessageMeta = Types.of(MessageMeta.class).desc();
    private static final ClassDesc CD_ParamMeta = Types.of(ParamMeta.class).desc();

    private static final ClassTypeRef Type_ContractShape = Types.of(ContractShape.class);
    private static final ClassTypeRef Type_MessageShape = Types.of(MessageShape.class);

    private ContractWriter() {}

    static JavaFile write(ContractModel model) {
        return JavaFile.class_(
                ClassDesc.of(model.packageName(), model.simpleName() + "Contract"),
                class_ -> class_.withAnnotation(asAnnotationUse(model))
                        .withModifiers(Modifier.PUBLIC, Modifier.FINAL)
                        .withField("SHAPE", Type_ContractShape, field -> field.withModifiers(
                                        Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                .withInitializer(asContractShapeExpr(model)))
                        .withConstructor(constructor -> constructor.withModifiers(Modifier.PRIVATE)));
    }

    private static Expr asContractShapeExpr(ContractModel model) {
        return new_(
                Type_ContractShape,
                staticCall(
                        Types.LIST,
                        "of",
                        model.messages().stream()
                                .map(ContractWriter::asMessageShapeExpr)
                                .toList()));
    }

    private static Expr asMessageShapeExpr(ContractModel.MessageModel message) {
        return new_(
                Type_MessageShape,
                Exprs.literal(message.key()),
                Exprs.literal(message.methodName()),
                staticCall(
                        Types.LIST,
                        "of",
                        message.params().stream()
                                .map(ContractWriter::asParamExpr)
                                .toList()));
    }

    private static Expr asParamExpr(ContractModel.ParamModel param) {
        return Exprs.literal(param.name());
    }

    private static AnnotationUse asAnnotationUse(ContractModel model) {
        return new AnnotationBuilder(CD_ContractMeta)
                .withMember(
                        "value",
                        array(model.messages().stream()
                                .map(ContractWriter::asAnnotationValue)
                                .toList()))
                .build();
    }

    private static SingleAnnotationValue asAnnotationValue(ContractModel.MessageModel message) {
        return AnnotationValues.nested(CD_MessageMeta, annotation -> annotation
                .withMember("key", literal(message.key()))
                .withMember("method", literal(message.methodName()))
                .withMember(
                        "params",
                        array(message.params().stream()
                                .map(ContractWriter::asAnnotationValue)
                                .toList())));
    }

    private static SingleAnnotationValue asAnnotationValue(ContractModel.ParamModel param) {
        return AnnotationValues.nested(CD_ParamMeta, annotation -> annotation
                .withMember("name", literal(param.name()))
                .withMember("type", classValue(param.type())));
    }

    private static SingleAnnotationValue classValue(TypeMirror mirror) {
        var asString =
                switch (Descriptors.toTypeRef(mirror)) {
                    case PrimitiveTypeRef primitive -> primitive.sourceName();
                    case ClassTypeRef(var desc, var _) -> desc.toString();
                    default -> throw new IllegalArgumentException("Raw class expected, got: " + mirror);
                };
        return AnnotationValues.literal(asString);
    }
}
