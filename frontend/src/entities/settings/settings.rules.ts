import type {
  SystemBooleanSettingKey,
  SystemSettings,
} from "./settings.types";

export function toggleSystemBooleanSetting(
  settings: SystemSettings,
  key: SystemBooleanSettingKey
): SystemSettings {
  return {
    ...settings,
    [key]: !settings[key],
  };
}
