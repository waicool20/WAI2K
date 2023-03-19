import type { AxiosStatic } from "axios";
import type { PiniaPluginContext } from "pinia";
import { inject } from "vue";

declare module "pinia" {
  export interface PiniaCustomProperties {
    $api: string | undefined;
    axios: AxiosStatic | undefined;
  }
}

export function InjectApiPlugin(context: PiniaPluginContext) {
  context.pinia.use(({ app }) => {
    const axios: AxiosStatic | undefined = app.config.globalProperties.axios;
    const $api: string | undefined = app.config.globalProperties.$api;
    console.log(axios);
    console.log($api);
    return {
      axios,
      $api,
    };
  });
}
