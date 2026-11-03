// Ask Cochilo's on-device model, delivered by the Play Store on demand (docs/assistant.md).
// The model file (assets/model.task, about 0.5 GB) is not in git: the Release workflow puts it
// here when the ASSISTANT_MODEL_URL secret is set. Without it the pack carries only README.txt,
// and the app says the model isn't in this build.
plugins {
    id("com.android.asset-pack")
}

assetPack {
    packName.set("assistant_model")
    dynamicDelivery {
        deliveryType.set("on-demand")
    }
}
