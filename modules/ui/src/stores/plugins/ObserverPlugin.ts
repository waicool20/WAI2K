import type { PiniaPluginContext } from "pinia";

const observableStores = ["profile", "config"];
export function ObserverPlugin(context: PiniaPluginContext) {
  context.pinia.use(({ store }) => {
    if (observableStores.indexOf(store.$id) !== -1) {
      store.$subscribe(() => {
        // react to store changes
        console.log(store.$id + " observed");
      });
    }
  });
}
