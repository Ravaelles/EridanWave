package Lifecycle

import bwapi.BWClient

object EridanBWClient extends BWClient(EridanEventListener)
