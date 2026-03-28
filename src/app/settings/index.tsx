import { useRouter } from "expo-router"
import { ScrollView, View } from "react-native"

import { SettingsRow } from "@/components/patterns/settings-row"
import { SETTINGS_CATEGORY_ROUTES } from "@/modules/settings/settings.routes"

export default function SettingsScreen() {
  const router = useRouter()

  return (
    <ScrollView
      className="flex-1 bg-background"
      contentContainerStyle={{ paddingBottom: 40 }}
    >
      <View className="py-2">
        {SETTINGS_CATEGORY_ROUTES.map((route) => (
          <SettingsRow
            key={route.name}
            title={route.title}
            description={route.description}
            onPress={() => router.push(`/settings/${route.name}`)}
          />
        ))}
      </View>
    </ScrollView>
  )
}
