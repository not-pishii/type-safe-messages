package me.supcheg.messages.processor;

import javax.lang.model.type.TypeMirror;
import java.util.List;

record ContractModel(String packageName, String simpleName, String typeParamName, List<MessageModel> messages) {

    record MessageModel(String key, String methodName, List<ParamModel> params) {
    }

    record ParamModel(String name, TypeMirror type) {
    }
}
