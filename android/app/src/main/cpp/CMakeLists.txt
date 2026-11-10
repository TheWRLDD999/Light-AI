
cmake_minimum_required(VERSION 3.22.1)

project(lightai)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_library(
    lightai
    SHARED
    native-lib.cpp
)

target_include_directories(lightai PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/include/llama
    ${CMAKE_CURRENT_SOURCE_DIR}/include/ggml
)

find_library(log-lib log)

add_library(llama SHARED IMPORTED)

set_target_properties(llama PROPERTIES
    IMPORTED_LOCATION
    ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/armeabi-v7a/libllama.so
)

target_link_libraries(
    lightai
    llama
    ${log-lib}
)
