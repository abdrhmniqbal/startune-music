import { Redirect } from "expo-router"
import { useEffect } from "react"

import { openPlayer } from "@/modules/ui/ui.store"

export default function NotificationClickRoute() {
  useEffect(() => {
    openPlayer()
  }, [])

  return <Redirect href="/(main)/(home)" />
}
