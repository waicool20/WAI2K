import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import { library } from "@fortawesome/fontawesome-svg-core";
import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";
import { fas } from "@fortawesome/free-solid-svg-icons";
import { far } from "@fortawesome/free-regular-svg-icons";
import { fab } from "@fortawesome/free-brands-svg-icons";

import "./assets/main.css";
import VueAxios from "vue-axios";
import Axios from "axios";
import { ObserverPlugin } from "./stores/plugins/ObserverPlugin";
import { InjectApiPlugin } from "./stores/plugins/InjectApiPlugin";

library.add(fas);
library.add(far);
library.add(fab);

const app = createApp(App);
app.use(router);

app.use(VueAxios, Axios);
app.provide("axios", app.config.globalProperties.axios);
app.config.globalProperties.$api = "http://localhost:17555";
app.provide("$api", app.config.globalProperties.$api);

const store = createPinia();
store.use(InjectApiPlugin);
store.use(ObserverPlugin);
app.use(store);

app.component("font-awesome-icon", FontAwesomeIcon);
app.mount("#app");
