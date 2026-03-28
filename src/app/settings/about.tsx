import * as Application from "expo-application"
import Constants from "expo-constants"
import { Image } from "expo-image"
import { Linking, ScrollView, Text, View } from "react-native"

import appIcon from "@/assets/icon.png"
import { SettingsRow } from "@/components/patterns/settings-row"

export default function AboutSettingsScreen() {
  const appName = Constants.expoConfig?.name || "Startune Music"
  const version =
    Application.nativeApplicationVersion ||
    Constants.nativeAppVersion ||
    Constants.expoConfig?.version
  const repositoryUrl = "https://github.com/abdrhmniqbal/startune-music"

  return (
    <ScrollView className="flex-1 bg-background">
      <View className="flex-row items-center gap-6 bg-background px-6 py-4">
        <Image
          source={appIcon}
          style={{ width: 64, height: 64 }}
          contentFit="contain"
        />
        <View className="flex-1">
          <Text className="text-[17px] font-normal text-foreground">
            {appName}
          </Text>
          <Text className="mt-1 text-[13px] leading-5 text-muted">
            v{version || "Unknown"}
          </Text>
        </View>
      </View>

      <SettingsRow
        onPress={() => {
          void Linking.openURL(repositoryUrl)
        }}
        title="GitHub"
        description="Project source code."
        className="py-6"
      />
    </ScrollView>
  )
}
