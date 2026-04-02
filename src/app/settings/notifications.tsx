import { Switch } from "heroui-native"
import { ScrollView, View } from "react-native"

import { SettingsRow } from "@/components/patterns/settings-row"
import { dismissIndexerProgressNotification } from "@/modules/indexer/indexer-notification.service"
import { setIndexerNotificationsEnabled } from "@/modules/settings/indexer-notifications"
import { useSettingsStore } from "@/modules/settings/settings.store"

export default function NotificationSettingsScreen() {
  const indexerNotificationsEnabled = useSettingsStore(
    (state) => state.indexerNotificationsEnabled
  )

  return (
    <ScrollView className="flex-1 bg-background">
      <View className="py-2">
        <SettingsRow
          title="Indexer Notifications"
          description={
            indexerNotificationsEnabled
              ? "Show system notifications while indexing."
              : "Hide indexing notifications from your system tray."
          }
          onPress={undefined}
          showChevron={false}
          rightContent={
            <Switch
              isSelected={indexerNotificationsEnabled}
              onSelectedChange={(isSelected) => {
                void setIndexerNotificationsEnabled(isSelected)

                if (!isSelected) {
                  void dismissIndexerProgressNotification()
                }
              }}
            />
          }
        />
      </View>
    </ScrollView>
  )
}
