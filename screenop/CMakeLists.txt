cmake_minimum_required(VERSION 3.4.1)

add_library( # Sets the name of the library.
             ffmpeg-handle

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/rtmp_handle.cpp)

#引用的头文件
include_directories(
                    src/main/cpp/include
                    )

add_library(rtmp SHARED IMPORTED)
set_target_properties(rtmp
  PROPERTIES IMPORTED_LOCATION
  ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI}/librtmp.so)

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in this
# build script, prebuilt third-party libraries, or system libraries.
# 链接库
target_link_libraries( # Specifies the target library.
                       ffmpeg-handle

                       rtmp
                       ${log-lib} )