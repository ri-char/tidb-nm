package main

/*
#cgo LDFLAGS: ../target/release/libtikv_bpf.a -ldl -ludev -lrt -lm
#include "./tikv_bpf.h"
*/
import "C"
import (
	"context"
	"fmt"
	"sync/atomic"
	"unsafe"

	"github.com/pingcap/tidb/plugin"
	"github.com/pingcap/tidb/sessionctx/variable"
)

type CChar C.char

var connection int32

func Validate(ctx context.Context, m *plugin.Manifest) error {
	return nil
}

func OnInit(ctx context.Context, manifest *plugin.Manifest) error {
	C.c_start_monitor()

	sv := &variable.SysVar{
		Name:  "net_stat",
		Scope: variable.ScopeSession,
		Value: "",
		Type:  variable.TypeStr,
		GetSession: func(vars *variable.SessionVars) (string, error) {
			return C.GoString(C.c_get_stats()), nil

		},
	}
	variable.RegisterSysVar(sv)
	atomic.SwapInt32(&connection, 0)
	return nil
}
