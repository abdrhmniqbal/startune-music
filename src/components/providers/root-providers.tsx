import { QueryClientProvider } from "@tanstack/react-query"

import { queryClient } from "@/lib/tanstack-query"

import { DatabaseProvider } from "./database-provider"

export function RootProviders({
  children,
  onDatabaseReady,
  onDatabaseError,
}: {
  children: React.ReactNode
  onDatabaseReady?: () => void
  onDatabaseError?: () => void
}) {
  return (
    <QueryClientProvider client={queryClient}>
      <DatabaseProvider onReady={onDatabaseReady} onError={onDatabaseError}>
        {children}
      </DatabaseProvider>
    </QueryClientProvider>
  )
}
