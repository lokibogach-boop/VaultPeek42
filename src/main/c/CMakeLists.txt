cmake_minimum_required(VERSION 3.15)
project(VaultPeekNative C)

if(NOT DEFINED JAVA_HOME)
    set(JAVA_HOME "$ENV{JAVA_HOME}")
endif()
if(NOT JAVA_HOME)
    message(FATAL_ERROR "JAVA_HOME must point to a JDK 21 installation")
endif()

file(TO_CMAKE_PATH "${JAVA_HOME}" JAVA_HOME_CMAKE)

if(WIN32)
    set(JAVA_PLATFORM_INCLUDE "win32")
elseif(APPLE)
    set(JAVA_PLATFORM_INCLUDE "darwin")
else()
    set(JAVA_PLATFORM_INCLUDE "linux")
endif()

add_subdirectory(cubiomes)

add_library(vaultpeeknative SHARED vaultpeek_jni.c)
target_include_directories(vaultpeeknative PRIVATE
    "${JAVA_HOME_CMAKE}/include"
    "${JAVA_HOME_CMAKE}/include/${JAVA_PLATFORM_INCLUDE}"
    "${CMAKE_CURRENT_SOURCE_DIR}/cubiomes"
    "${CMAKE_CURRENT_SOURCE_DIR}/cubiomes/loot"
)
target_link_libraries(vaultpeeknative PRIVATE cubiomes_static)
set_target_properties(vaultpeeknative PROPERTIES OUTPUT_NAME "vaultpeeknative")
