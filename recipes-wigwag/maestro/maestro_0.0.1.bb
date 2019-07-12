DESCRIPTION = "maestro is a runtime / container manager for deviceOS"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://m/LICENSE;md5=1dece7821bf3fd70fe1309eaa37d52a2"


DEPENDS="deviceos-users"
RDEPENDS_${PN}+="bash twlib"
inherit go pkgconfig gitpkgv update-rc.d

INITSCRIPT_NAME = "maestro.sh"
INITSCRIPT_PARAMS = "defaults 85 15"


CGO_ENABLED = "1"

PV = "1.0+git${SRCPV}"
PKGV = "1.0+git${GITPKGV}"


PR = "r0"

FILES_${PN} += "/wigwag/system/bin/* /wigwag/system/lib/* ${INIT_D_DIR}/*"

SRC_URI="git://git@github.com/armPelionEdge/maestro.git;protocol=ssh;branch=grease_disable;name=m;destsuffix=git/m \
git://git@github.com/armPelionEdge/rallypointwatchdogs.git;protocol=ssh;branch=master;name=wd;destsuffix=git/wd \
file://maestro.sh \
"

SRCREV_FORMAT="m-wd"
SRCREV_m="${AUTOREV}"
SRCREV_wd="${AUTOREV}"

S = "${WORKDIR}/git"

WSYS="/wigwag/system"
WSB="/wigwag/system/bin"
WSL="/wigwag/system/lib"


LOG="/tmp/maestro_0.0.1.bb.log"

do_package_qa () {
  echo "done"
}

do_configure() {
    cd ../git
    TOP=`pwd`
    S_WD="${TOP}/wd"
    S_M="${TOP}/m"

    cd $S_M

    export LD="${CXX}"
    if [ "${TARGET_ARCH}" = "arm" ]; then
        CONFIG_OPTIONS="--host=arm ${ARCHFLAGS}"
    elif [ "${TARGET_ARCH}" = "x86_64" ]; then
         CONFIG_OPTIONS="--host=x64 ${ARCHFLAGS}"
    else
          CONFIG_OPTIONS="--host=ia32  ${ARCHFLAGS}"
    fi
    export CONFIG_OPTIONS="${CONFIG_OPTIONS}"

    # remove the /vendor/maestroSpecs dir, b/c we want this to use the same folder
    # as the plugins (watchdog, etc.)
    DEBUG=1 DEBUG2=1 ./build.sh preprocess_only
    # wipe out the src directories, seems to cause confusion with Go compiler in
    rm -rf src

    # Yocto build

    cd ${WORKDIR}
    mkdir -p go-workspace/bin
    mkdir -p go-workspace/pkg
    mkdir -p go-workspace/src
    mkdir -p go-workspace/src/github.com/armPelionEdge  
    mv "${S_M}" go-workspace/src/github.com/armPelionEdge/maestro
    mv go-workspace/src/github.com/armPelionEdge/maestro/vendor/github.com/armPelionEdge/greasego go-workspace/src/github.com/armPelionEdge/greasego
    mv go-workspace/src/github.com/armPelionEdge/maestro/vendor/github.com/armPelionEdge/maestroSpecs go-workspace/src/github.com/armPelionEdge/maestroSpecs
    mv go-workspace/src/github.com/armPelionEdge/maestro/vendor/github.com/armPelionEdge/mustache go-workspace/src/github.com/armPelionEdge/mustache
    rm -rf "${S_WD}/vendor/github.com/armPelionEdge/maestroSpecs"
    mv "${S_WD}" go-workspace/src/github.com/armPelionEdge/rallypointwatchdogs
}

do_compile() {
    TOP=`pwd`
    S_WD="${TOP}/wd"
    S_M="${TOP}/m"
    S_SPECS="${TOP}/specs"
    cd ..
    WORKSPACE="`pwd`/go-workspace"
    export CGO_ENABLED=1
    export GOPATH="$WORKSPACE"
    export GOBIN="$WORKSPACE/bin"
    cd go-workspace/src
    # when not doing a debug - get rid of the DEBUG vars
    # On 'thud': for some reason the GOARCH is using the host not the target
    export GOARCH=`echo $AR | awk -F '-' '{print $1}'`
    go env
    cd "$WORKSPACE"/bin
    go build -x github.com/armPelionEdge/maestro/maestro
    cd "$WORKSPACE"/src/github.com/armPelionEdge/rallypointwatchdogs
    # TODO -  only build what we need for the platform
    ./build.sh
}

do_install() {
    echo ${D}/wigwag/system/lib > /tmp/dwlib
    echo ${D}/wigwag/system/bin >> /tmp/dwlib
    install -d ${D}/wigwag/
    install -d ${D}/wigwag/system
    install -d ${D}/wigwag/system/bin
    install -d ${D}/wigwag/system/lib
    install -d ${D}${INIT_D_DIR}
    install -m 0755 ${S}/../maestro.sh ${D}${INIT_D_DIR}/maestro.sh
    WORKSPACE=`pwd`/../go-workspace
    install -m 0755 "${WORKSPACE}/src/github.com/armPelionEdge/rallypointwatchdogs/rp100/rp100wd.so" "${D}/wigwag/system/lib"
    install -m 0755 "${WORKSPACE}/src/github.com/armPelionEdge/rallypointwatchdogs/dummy/dummywd.so" "${D}/wigwag/system/lib"
    install -m 0755 "${WORKSPACE}/bin/maestro" "${D}/wigwag/system/bin"
}
