import type { AxiosStatic } from "axios";
import type { PiniaPluginContext } from "pinia";

declare module "pinia" {
  export interface PiniaCustomProperties {
    $api: string;
    axios: AxiosStatic;
  }
}

export function InjectApiPlugin(context: PiniaPluginContext) {
  context.pinia.use(({ app }) => {
    return {
      axios: app.config.globalProperties.axios,
      $api: app.config.globalProperties.$api,
    };
  });
}
