import * as Application from "expo-application"
import Constants from "expo-constants"
import { Image } from "expo-image"
import { PressableFeedback } from "heroui-native"
import { Linking, ScrollView, Text, View } from "react-native"

import appIcon from "@/assets/icon.png"
import LocalChevronRightIcon from "@/components/icons/local/chevron-right"
import { useThemeColors } from "@/hooks/use-theme-colors"

export default function AboutSettingsScreen() {
  const theme = useThemeColors()
  const appName = Constants.expoConfig?.name || "Euphony Music"
  const version =
    Application.nativeApplicationVersion ||
    Constants.nativeAppVersion ||
    Constants.expoConfig?.version
  const repositoryUrl = "https://github.com/abdrhmniqbal/euphony-music"

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

      <PressableFeedback
        onPress={() => {
          void Linking.openURL(repositoryUrl)
        }}
        className="flex-row items-center bg-background px-6 py-6 active:opacity-70"
      >
        <View className="flex-1 gap-1">
          <Text className="text-[17px] font-normal text-foreground">
            GitHub
          </Text>
          <Text className="text-[13px] leading-5 text-muted">
            Project source code.
          </Text>
        </View>
        <LocalChevronRightIcon
          fill="none"
          width={20}
          height={20}
          color={theme.muted}
        />
      </PressableFeedback>
    </ScrollView>
  )
}
