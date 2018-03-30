#!/bin/bash

USAGE="Usage: $0 platform configuration destination"

PLATFORM="${1?$USAGE}"
CONFIG="${2?$USAGE}"
DEST="${3?$USAGE}"
TARGET="${4-UNKNOWN}"

echo "PLATFORM = $PLATFORM"
echo "CONFIG = $CONFIG"
echo "DEST = $DEST"
echo "TARGET = $TARGET"

set -e -x

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

# Ensure Unix paths on Windows from here on
if [[ $OS = "Windows_NT" ]]; then
  DEST="$(cygpath --unix --absolute "$DEST")"
  SCRIPT_DIR="$(cygpath --unix --absolute "$SCRIPT_DIR")"
fi

SOURCE_ROOT="$SCRIPT_DIR/../.."
BUILD_ROOT="$SOURCE_ROOT/build/$PLATFORM"

SRCJAR="$BUILD_ROOT/bindings/java/carbon_java_lib.jar"
SRCJARBINDING="$BUILD_ROOT/bin/libcarbon_java_bindings.so"
SRCCARBONX="$BUILD_ROOT/bin/carbonx"

if [[ $OS = "Windows_NT" ]]; then
  case $TARGET in 
    UNKNOWN) LIBPREFIX=carbon
             DYNLIBSUFFIX=.dll
             STATLIBSUFFIX=.lib

             SRCBIN="$BUILD_ROOT/bin/$CONFIG"
             SRCLIB="$BUILD_ROOT/lib/$CONFIG"
             SRCDYNLIB="$BUILD_ROOT/bin/$CONFIG"
             ;;
    ANDROID) LIBPREFIX=libcarbon
             DYNLIBSUFFIX=.so
             STATLIBSUFFIX=.a

             SRCBIN="$BUILD_ROOT/bin"
             SRCLIB="$BUILD_ROOT/lib"
             SRCDYNLIB="$BUILD_ROOT/lib"
             ;;
    *) echo "We should never reach this point"
       echo "The forth parameter should be empty to ANDROID"
       ;;
  esac
else
  LIBPREFIX=libcarbon

  if [[ $(uname) = Linux ]]; then
    DYNLIBSUFFIX=.so
  else
    DYNLIBSUFFIX=.dylib
  fi

  STATLIBSUFFIX=.a

  SRCBIN="$BUILD_ROOT/bin"
  SRCLIB="$BUILD_ROOT/lib"
  SRCDYNLIB="$BUILD_ROOT/lib"
fi

SRCINC="$SOURCE_ROOT/source/public"
SRCPRIVINC="$SOURCE_ROOT/source/core/include"

DESTPUBLIB="$DEST/public/lib"
DESTPUBBIN="$DEST/public/bin"
DESTPUBINC="$DEST/public/include"
DESTPRIVLIB="$DEST/private/lib"
DESTPRIVINC="$DEST/private/include"

printf "\nCopying files to drop location\n"

# N.B. no long option for -p (parents) on OSX.
mkdir -p "$DESTPUBLIB" "$(dirname "$DESTPUBINC")" "$DESTPRIVLIB" "$(dirname "$DESTPRIVINC")" "$DESTPUBLIB"

# N.B. no long option for -v (verbose) and -p (preserve) on OSX.
CPOPT="-v -p"

cp $CPOPT "$SRCDYNLIB"/$LIBPREFIX*$DYNLIBSUFFIX "$DESTPUBLIB"
# On Windows and not Android, copy import libraries
if [[ $OS = "Windows_NT" ]]; then
  if [[ $TARGET != "ANDROID" ]]; then
    cp $CPOPT "$SRCLIB"/$LIBPREFIX*.lib "$DESTPUBLIB"
  else
    cp $CPOPT "$ANDROID_NDK/sources/cxx-stl/llvm-libc++/libs/armeabi-v7a/libc++_shared.so" "$DESTPUBLIB"
  fi
fi

# Copy .jar if available
[[ -e $SRCJAR ]] && cp $CPOPT "$SRCJAR" "$DESTPUBLIB"
[[ -e $SRCJARBINDING ]] && cp $CPOPT "$SRCJARBINDING" "$DESTPUBLIB"

# copy carbonx if available
[[ -e $SRCCARBONX ]] && cp $CPOPT "$SRCCARBONX" "$DESTPUBBIN"

# N.B. no long option for -R (recursive) on OSX.
cp $CPOPT -R "$SRCINC"* "$DESTPUBINC"

# N.B. Using '-I -n 1' and replacement instead of "cp --target" since --target
# is not available on OSX.
find "$SRCLIB" -type f -name \*$STATLIBSUFFIX -not -name $LIBPREFIX\* -print0 |
  xargs -n 1 -0 -I % cp % "$DESTPRIVLIB"

cp $CPOPT -R "$SRCPRIVINC" "$DESTPRIVINC"
