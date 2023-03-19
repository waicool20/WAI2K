import type { AxiosStatic } from "axios";
import type { PiniaPluginContext } from "pinia";

declare module "pinia" {
  export interface PiniaCustomProperties {
    $api: string;
    axios: AxiosStatic;
  }
}

export function injectApiPlugin(context: PiniaPluginContext) {
  context.store.axios = context.app.axios;
  context.store.$api = context.app.$api;
}
