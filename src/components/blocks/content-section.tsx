import type { ReactNode } from "react"
import { cn } from "tailwind-variants"

import { EmptyState } from "@/components/ui/empty-state"
import { SectionHeader } from "@/components/ui/section-header"

interface EmptyStateConfig {
  icon: ReactNode
  title: string
  message: string
}

interface ContentSectionProps<T> {
  title: string
  onViewMore?: () => void
  data: T[]
  renderContent: (data: T[]) => ReactNode
  emptyState: EmptyStateConfig
  className?: string
  titleClassName?: string
}

export function ContentSection<T>({
  title,
  onViewMore,
  data,
  renderContent,
  emptyState,
  className,
  titleClassName,
}: ContentSectionProps<T>) {
  return (
    <>
      <SectionHeader
        title={title}
        onViewMore={data.length > 0 ? onViewMore : undefined}
        className={cn("px-4", titleClassName)}
      />
      {data.length > 0 ? (
        renderContent(data)
      ) : (
        <EmptyState
          icon={emptyState.icon}
          title={emptyState.title}
          message={emptyState.message}
          className={cn("mb-8 py-8", className)}
        />
      )}
    </>
  )
}
