import * as React from "react"
import { useCallback, useRef } from "react"
import { useBottomSheetInternal } from "@gorhom/bottom-sheet"
import { Input, PressableFeedback, TextField } from "heroui-native"
import {
  TextInput,
  View,
  findNodeHandle,
  type BlurEvent,
  type FocusEvent,
} from "react-native"

import { useThemeColors } from "@/hooks/use-theme-colors"
import LocalCancelCircleSolidIcon from "@/components/icons/local/cancel-circle-solid"
import LocalSearchIcon from "@/components/icons/local/search"

import type { SheetSearchInputProps } from "./types"

export function SheetSearchInput({
  inputKey,
  searchQuery,
  setSearchQuery,
}: SheetSearchInputProps) {
  const theme = useThemeColors()
  const { animatedKeyboardState, textInputNodesRef } = useBottomSheetInternal()
  const inputRef = useRef<TextInput>(null)

  const handleOnFocus = useCallback(
    (e: FocusEvent) => {
      animatedKeyboardState.set((state) => ({
        ...state,
        target: e.nativeEvent.target,
      }))
    },
    [animatedKeyboardState]
  )

  const handleOnBlur = useCallback(
    (e: BlurEvent) => {
      const keyboardState = animatedKeyboardState.get()
      const currentFocusedInput = findNodeHandle(
        TextInput.State.currentlyFocusedInput() as TextInput | null
      )
      const shouldRemoveCurrentTarget =
        keyboardState.target === e.nativeEvent.target
      const shouldIgnoreBlurEvent =
        currentFocusedInput &&
        textInputNodesRef.current.has(currentFocusedInput)

      if (shouldRemoveCurrentTarget && !shouldIgnoreBlurEvent) {
        animatedKeyboardState.set((state) => ({
          ...state,
          target: undefined,
        }))
      }
    },
    [animatedKeyboardState, textInputNodesRef]
  )

  return (
    <TextField className="absolute top-0 right-0 left-0 px-5 pt-2">
      <View className="w-full flex-row items-center">
        <Input
          key={inputKey}
          ref={inputRef}
          variant="secondary"
          placeholder="Search tracks..."
          onChangeText={setSearchQuery}
          className="flex-1 pr-10 pl-10"
          autoCapitalize="none"
          autoCorrect={false}
          onFocus={handleOnFocus}
          onBlur={handleOnBlur}
        />
        <View className="absolute left-3.5" pointerEvents="none">
          <LocalSearchIcon
            fill="none"
            width={20}
            height={20}
            color={theme.muted}
          />
        </View>
        {searchQuery.length > 0 ? (
          <PressableFeedback
            className="absolute right-3 p-1"
            onPress={() => {
              inputRef.current?.clear()
              setSearchQuery("")
            }}
            hitSlop={12}
          >
            <LocalCancelCircleSolidIcon
              fill="none"
              width={18}
              height={18}
              color={theme.muted}
            />
          </PressableFeedback>
        ) : null}
      </View>
    </TextField>
  )
}
