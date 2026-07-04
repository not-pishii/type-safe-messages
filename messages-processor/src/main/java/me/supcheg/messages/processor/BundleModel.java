package me.supcheg.messages.processor;

import me.supcheg.messages.annotation.Resolution;

import javax.lang.model.element.TypeElement;
import java.util.List;

record BundleModel(
    TypeElement contractInterface,
    ContractModel contract,
    List<String> localeTags,
    Resolution resolution,
    String resources,
    String packageName) {
}
