import { useEffect } from "react"

import { registerBootstrapListeners } from "@/modules/bootstrap/bootstrap.runtime"

export function BootstrapEffects() {
  useEffect(() => {
    return registerBootstrapListeners()
  }, [])

  return null
}
