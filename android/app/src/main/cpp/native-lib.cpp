
#include <jni.h>
#include <string>
#include <vector>
#include <mutex>

#include "llama.h"

static llama_model * model = nullptr;
static llama_context * context = nullptr;
static const llama_vocab * vocab = nullptr;

static std::mutex aiMutex;

extern "C"
JNIEXPORT jstring JNICALL
Java_com_thewrldd999_lightai_MainActivity_getEngineStatus(
        JNIEnv* env, jobject) {
    return env->NewStringUTF(
        "Light AI native engine connected!"
    );
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_thewrldd999_lightai_MainActivity_loadModel(
        JNIEnv* env, jobject, jstring modelPath) {

    std::lock_guard<std::mutex> lock(aiMutex);

    const char * path = env->GetStringUTFChars(modelPath, nullptr);

    if (path == nullptr) {
        return env->NewStringUTF("Could not read model path.");
    }

    if (context != nullptr) {
        llama_free(context);
        context = nullptr;
    }

    if (model != nullptr) {
        llama_model_free(model);
        model = nullptr;
        vocab = nullptr;
    }

    llama_backend_init();

    llama_model_params modelParams =
        llama_model_default_params();

    modelParams.n_gpu_layers = 0;
    modelParams.load_mode = LLAMA_LOAD_MODE_MMAP;

    model = llama_model_load_from_file(path, modelParams);

    env->ReleaseStringUTFChars(modelPath, path);

    if (model == nullptr) {
        return env->NewStringUTF(
            "Model loading failed. Check the model file."
        );
    }

    vocab = llama_model_get_vocab(model);

    llama_context_params contextParams =
        llama_context_default_params();

    contextParams.n_ctx = 512;
    contextParams.n_batch = 128;
    contextParams.n_ubatch = 128;
    contextParams.n_threads = 2;
    contextParams.n_threads_batch = 2;

    context = llama_init_from_model(model, contextParams);

    if (context == nullptr) {
        llama_model_free(model);
        model = nullptr;
        vocab = nullptr;

        return env->NewStringUTF(
            "Model loaded, but context creation failed."
        );
    }

    return env->NewStringUTF(
        "Light AI model loaded successfully!"
    );
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_thewrldd999_lightai_MainActivity_generateResponse(
        JNIEnv* env, jobject, jstring userMessage) {

    std::lock_guard<std::mutex> lock(aiMutex);

    if (model == nullptr || context == nullptr || vocab == nullptr) {
        return env->NewStringUTF(
            "Please load the AI model first."
        );
    }

    const char * message =
        env->GetStringUTFChars(userMessage, nullptr);

    if (message == nullptr) {
        return env->NewStringUTF("Could not read your message.");
    }

    std::string prompt =
        "<|im_start|>system\n"
        "You are Light AI, a friendly, helpful and curious assistant. "
        "Be honest, patient, creative and encouraging. "
        "Keep answers clear and useful.\n"
        "<|im_end|>\n"
        "<|im_start|>user\n";

    prompt += message;
    prompt += "\n<|im_end|>\n<|im_start|>assistant\n";

    env->ReleaseStringUTFChars(userMessage, message);

    llama_memory_clear(llama_get_memory(context), true);

    int32_t needed = llama_tokenize(
        vocab,
        prompt.c_str(),
        (int32_t) prompt.size(),
        nullptr,
        0,
        true,
        false
    );

    if (needed >= 0) {
        return env->NewStringUTF("Could not tokenize the message.");
    }

    std::vector<llama_token> tokens(-needed);

    int32_t tokenCount = llama_tokenize(
        vocab,
        prompt.c_str(),
        (int32_t) prompt.size(),
        tokens.data(),
        (int32_t) tokens.size(),
        true,
        false
    );

    if (tokenCount <= 0) {
        return env->NewStringUTF("Message tokenization failed.");
    }

    tokens.resize(tokenCount);

    if (llama_decode(
        context,
        llama_batch_get_one(tokens.data(), tokenCount)
    ) != 0) {
        return env->NewStringUTF("Prompt processing failed.");
    }

    llama_sampler * sampler =
        llama_sampler_chain_init(
            llama_sampler_chain_default_params()
        );

    llama_sampler_chain_add(
        sampler,
        llama_sampler_init_top_k(40)
    );

    llama_sampler_chain_add(
        sampler,
        llama_sampler_init_top_p(0.9f, 1)
    );

    llama_sampler_chain_add(
        sampler,
        llama_sampler_init_temp(0.7f)
    );

    llama_sampler_chain_add(
        sampler,
        llama_sampler_init_dist(LLAMA_DEFAULT_SEED)
    );

    std::string response;

    for (int i = 0; i < 96; i++) {

        llama_token token =
            llama_sampler_sample(sampler, context, -1);

        if (llama_vocab_is_eog(vocab, token)) {
            break;
        }

        char piece[256];

        int32_t pieceLength = llama_token_to_piece(
            vocab,
            token,
            piece,
            sizeof(piece),
            0,
            false
        );

        if (pieceLength > 0) {
            response.append(piece, pieceLength);
        }

        if (llama_decode(
            context,
            llama_batch_get_one(&token, 1)
        ) != 0) {
            break;
        }
    }

    llama_sampler_free(sampler);

    if (response.empty()) {
        response = "I couldn't generate a response. Please try again.";
    }

    return env->NewStringUTF(response.c_str());
}
