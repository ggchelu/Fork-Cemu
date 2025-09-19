#include "Common/precompiled.h"

// Android stub implementations for disabled features

#ifdef ANDROID

// Stub implementations for nn::olv functions
namespace nn {
    namespace olv {
        void load() {
            // Stub implementation - nn_olv disabled on Android
        }
        
        void unload() {
            // Stub implementation - nn_olv disabled on Android
        }
    }
}

// Stub implementations for nlibcurl functions
namespace nlibcurl {
    void load() {
        // Stub implementation - nlibcurl disabled on Android
    }
}

// Forward declare the NetworkService enum
enum class NetworkService {
    Nintendo_NASC = 0,
    Nintendo_ECS = 1,
    Nintendo_NUS = 2,
    Nintendo_IAS = 3,
    Nintendo_PUSH = 4,
    Nintendo_NIM = 5,
    Nintendo_NEX = 6
};

// Stub implementations for CurlRequestHelper class
class CurlRequestHelper {
public:
    enum SERVER_SSL_CONTEXT {
        SERVER_SSL_CONTEXT_NINTENDO
    };
    
    CurlRequestHelper();
    ~CurlRequestHelper();
    bool initate(NetworkService networkService, std::string url, SERVER_SSL_CONTEXT sslContext);
    bool submitRequest(bool followRedirect = true);
    void addHeaderField(const char* name, std::string_view value);
    void addPostField(const char* name, std::string_view value);
};

// Provide actual symbol definitions
CurlRequestHelper::CurlRequestHelper() {
    // Stub implementation - CURL disabled on Android
}

CurlRequestHelper::~CurlRequestHelper() {
    // Stub implementation - CURL disabled on Android
}

bool CurlRequestHelper::initate(NetworkService networkService, std::string url, SERVER_SSL_CONTEXT sslContext) {
    // Stub implementation - CURL disabled on Android
    return false;
}

bool CurlRequestHelper::submitRequest(bool followRedirect) {
    // Stub implementation - CURL disabled on Android
    return false;
}

void CurlRequestHelper::addHeaderField(const char* name, std::string_view value) {
    // Stub implementation - CURL disabled on Android
}

void CurlRequestHelper::addPostField(const char* name, std::string_view value) {
    // Stub implementation - CURL disabled on Android
}

// Stub implementations for CURL functions used by iosu_boss
extern "C" {
    void* curl_easy_init() {
        // Stub implementation - CURL disabled on Android
        return nullptr;
    }
    
    int curl_easy_setopt(void* curl, int option, ...) {
        // Stub implementation - CURL disabled on Android
        return 0;
    }
    
    int curl_easy_perform(void* curl) {
        // Stub implementation - CURL disabled on Android
        return 0;
    }
    
    int curl_easy_getinfo(void* curl, int info, ...) {
        // Stub implementation - CURL disabled on Android
        return 0;
    }
    
    void curl_easy_cleanup(void* curl) {
        // Stub implementation - CURL disabled on Android
    }
    
    void* curl_slist_append(void* list, const char* string) {
        // Stub implementation - CURL disabled on Android
        return nullptr;
    }
    
    void curl_slist_free_all(void* list) {
        // Stub implementation - CURL disabled on Android
    }
}

#endif // ANDROID
