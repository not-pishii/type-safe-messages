package me.supcheg.messages.processor;

import me.supcheg.javafile.code.Expr;
import me.supcheg.javafile.type.ClassTypeRef;
import me.supcheg.javafile.type.Types;

import java.util.List;

import static me.supcheg.javafile.code.Exprs.field;
import static me.supcheg.javafile.code.Exprs.lambda;
import static me.supcheg.javafile.code.Exprs.literal;
import static me.supcheg.javafile.code.Exprs.literalNull;
import static me.supcheg.javafile.code.Exprs.new_;
import static me.supcheg.javafile.code.Exprs.switchExpr;

final class MethodSignatures {
    private static final ClassTypeRef Type_IllegalStateException = Types.of(IllegalStateException.class);

    private MethodSignatures() {}

    static Expr argumentsFunctionExpr(ContractModel.MessageModel message) {
        if (message.params().isEmpty()) {
            return lambda(List.of("name"), literalNull());
        }
        return lambda(List.of("name"), switchExpr(field("name"), sb -> {
            for (var param : message.params()) {
                sb.caseValue(literal(param.name()), field(param.name()));
            }
            sb.default_(cb -> cb.throw_(new_(Type_IllegalStateException, field("name"))));
        }));
    }
}
