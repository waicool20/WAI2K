import type { PiniaPluginContext } from "pinia";

export function ObserverPlugin(context: PiniaPluginContext) {
  context.pinia.use(({ store }) => {
    store.$subscribe(() => {
      // react to store changes
      console.log(store.$id);
    });
  });
}
