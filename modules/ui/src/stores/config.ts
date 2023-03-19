import { defineStore } from "pinia";

interface ScriptConfig {
  loopDelay: number;
  baseNavigationDelay: number;
  mouseDelay: number;
  defaultSimilarityThreshold: number;
  mapRunnerSimilarityThreshold: number;
  ocrThreshold: number;
  maxPostBattleClicks: number;
  idleAtHome: boolean;
}

interface GameRestartConfig {
  enabled: boolean;
  averageDelay: number;
  delayCoefficientThreshold: number;
  maxRestarts: number;
}

interface NotificationsConfig {
  onRestart: boolean;
  onStopCondition: boolean;
}

interface Config {
  current_profile: string;
  clear_console_on_start: boolean;
  show_console_on_start: boolean;
  debug_mode_enabled: boolean;
  capture_method: string;
  capture_compression_mode: string;
  last_device_serial: string;
  script_config: ScriptConfig;
  game_restart_config: GameRestartConfig;
  api_key: string;
  notifications_config: NotificationsConfig;
}

export const useConfigStore = defineStore("config", {
  state: (): Config => ({
    current_profile: "",
    clear_console_on_start: false,
    show_console_on_start: false,
    debug_mode_enabled: false,
    capture_method: "",
    capture_compression_mode: "",
    last_device_serial: "",
    script_config: {
      loopDelay: 0,
      baseNavigationDelay: 0,
      mouseDelay: 0,
      defaultSimilarityThreshold: 0,
      mapRunnerSimilarityThreshold: 0,
      ocrThreshold: 0,
      maxPostBattleClicks: 0,
      idleAtHome: false,
    },
    game_restart_config: {
      enabled: false,
      averageDelay: 0,
      delayCoefficientThreshold: 0,
      maxRestarts: 0,
    },
    api_key: "",
    notifications_config: {
      onRestart: false,
      onStopCondition: false,
    },
  }),
  actions: {
    async load() {
      const result = await this.axios.get(this.$api);
      this.$patch(result.data);
    },
    async checkApiKey() {
      console.log(this.api_key);
      try {
        const res = await this.axios.post(this.$api + "/yuubot/apikey", {
          apiKey: this.api_key,
        });
        if (res.status === 200) {
          return "success";
        }
      } catch (e) {
        // @ts-ignore
        if (e.response?.status === 406) {
          return "warning";
        }
        // @ts-ignore
        if (e.response?.status === 500) {
          return "error";
        }
      }
      return "";
    },
    async sendTestMessage(title: string = "", message: string = "") {
      try {
        await this.axios.post(this.$api + "/yuubot/message", {
          apiKey: this.api_key,
          title: title,
          message: message,
        });
      } catch (e) {
        console.log(e);
      }
    },
    setApiKey(value: string) {
      this.api_key = value;
    },
    setNotificationOnRestart(value: boolean) {
      this.notifications_config.onRestart = value;
    },
    setNotificationOnStopCondition(value: boolean) {
      this.notifications_config.onStopCondition = value;
    },
  },
  getters: {
    apiKey: (state): string => {
      return state.api_key;
    },
    notificationOnRestart: (state): boolean => {
      return state.notifications_config.onRestart;
    },
    notificationOnStopCondition: (state): boolean => {
      return state.notifications_config.onStopCondition;
    },
  },
});
