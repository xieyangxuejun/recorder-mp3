# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_VERBOSE_MAKEFILE on)
set(CMAKE_ANDROID_STL_TYPE ${CMAKE_ANDROID_STL_TYPE} stlport_static)

set(PATH_LIB_SO ${CMAKE_SOURCE_DIR}/src/main/jniLibs)
set(CPP_DIR src/main/cpp)
aux_source_directory(${CPP_DIR} SRC_LIST)

add_library( mp3lame SHARED IMPORTED )
set_target_properties(
            mp3lame
            PROPERTIES IMPORTED_LOCATION
            ${PATH_LIB_SO}/${ANDROID_ABI}/libmp3lame.so
            )
include_directories(
            ${CPP_DIR}
            )

add_library(recodermp3
            SHARED
            ${SRC_LIST}
            )

set(LIBS)
list(APPEND LIBS
    mp3lame
	jnigraphics
	log
)

target_link_libraries(recodermp3 ${LIBS})