import { File, Paths } from "expo-file-system"

export function createIndexerConfigFile(fileName: string) {
  return new File(Paths.document, fileName)
}

export async function loadIndexerConfig<T>(
  file: File,
  fallback: T,
  sanitize: (config: Partial<T>) => T
): Promise<T> {
  try {
    if (!file.exists) {
      return fallback
    }

    const raw = await file.text()
    const parsed = JSON.parse(raw) as Partial<T>
    return sanitize(parsed)
  } catch {
    return fallback
  }
}

export async function saveIndexerConfig<T>(
  file: File,
  config: T
): Promise<void> {
  if (!file.exists) {
    file.create({
      intermediates: true,
      overwrite: true,
    })
  }

  file.write(JSON.stringify(config), {
    encoding: "utf8",
  })
}
