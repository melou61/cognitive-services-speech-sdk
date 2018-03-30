set PATH=%PATH%;C:\Program Files (x86)\Microsoft Visual Studio\2017\Enterprise\VC\Tools\MSVC\14.13.26128\bin\HostX86\x86
rem set SOURCE_ROOT=%~dp0
set SOURCE_ROOT="%CD%"
mkdir build && ^
cd build && ^
cmake -G "NMake Makefiles" -Duse_default_uuid=ON -DCMAKE_SYSTEM_NAME=Android -DCMAKE_SYSTEM_VERSION=26 %SOURCE_ROOT% -DCMAKE_ANDROID_STL_TYPE=c++_shared -DOPENSSL_ROOT_DIR=%SOURCE_ROOT%\external\openssl\_install  -DCURL_LIBRARY=%SOURCE_ROOT%\external\curl\_install\lib\libcurl.so -DCURL_INCLUDE_DIR=%SOURCE_ROOT%\external\curl\_install\include && ^
cmake --build .
