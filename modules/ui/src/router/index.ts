import { createRouter, createWebHistory } from "vue-router";
import type { RouteRecordNormalized } from "vue-router";
import WelcomeView from "../views/WelcomeView.vue";
import AboutView from "../views/AboutView.vue";
import StatusView from "../views/StatusView.vue";
import LogisticsView from "../views/Profile/LogisticsView.vue";
import SidebarLayout from "../components/SidebarLayout.vue";
import { useProfileStore } from "@/stores/profile";
import { useConfigStore } from "@/stores/config";

const router = createRouter({
  // @ts-ignore
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: "/",
      name: "welcome",
      component: WelcomeView,
      meta: {
        title: "Welcome",
      },
    },
    {
      path: "/about",
      name: "about",
      component: AboutView,
      meta: {
        title: "About",
      },
    },
    {
      path: "/status",
      name: "status",
      component: StatusView,
      meta: {
        title: "Status",
      },
    },
    {
      path: "/profile",
      name: "profile",
      meta: {
        title: "Profile",
        store: useProfileStore,
      },
      component: SidebarLayout,
      props: { route: "profile" },
      children: [
        {
          path: "logistics",
          name: "logistics",
          meta: {
            title: "Logistics & assignments",
          },
          component: LogisticsView,
        },
        {
          path: "auto-battle",
          name: "auto-battle",
          meta: {
            title: "Auto battle",
          },
          component: () => import("../views/Profile/AutoBattleView.vue"),
        },
        {
          path: "combat",
          name: "combat",
          meta: {
            title: "Combat & draggers",
          },
          component: () => import("../views/Profile/CombatView.vue"),
        },
        {
          path: "combat-report",
          name: "combat-report",
          meta: {
            title: "Combat report",
          },
          component: () => import("../views/Profile/CombatReportView.vue"),
        },
        {
          path: "combat-sim",
          name: "combat-sim",
          meta: {
            title: "Combat simulation",
          },
          component: () => import("../views/Profile/CombatSimView.vue"),
        },
        {
          path: "factory",
          name: "factory",
          meta: {
            title: "Factory",
          },
          component: () => import("../views/Profile/FactoryView.vue"),
        },
        {
          path: "stop",
          name: "stop",
          meta: {
            title: "Stop settings & conditions",
          },
          component: () => import("../views/Profile/StopView.vue"),
        },
      ],
    },
    {
      path: "/preferences",
      name: "preferences",
      meta: {
        title: "Preferences",
        store: useConfigStore,
      },
      component: SidebarLayout,
      props: { route: "preferences" },
      children: [
        {
          path: "restart",
          name: "restart",
          meta: {
            title: "Game restart",
          },
          component: () => import("../views/Preferences/RestartView.vue"),
        },
        {
          path: "misc",
          name: "misc",
          meta: {
            title: "Misc",
          },
          component: () => import("../views/Preferences/MiscView.vue"),
        },
        {
          path: "script",
          name: "script",
          meta: {
            title: "Script",
          },
          component: () => import("../views/Preferences/ScriptView.vue"),
        },
        {
          path: "yuu-bot",
          name: "yuu-bot",
          meta: {
            title: "YuuBot",
          },
          component: () => import("../views/Preferences/YuuBotView.vue"),
        },
      ],
    },
  ],
});

router.beforeEach((to) => {
  console.log(to);
  const containStore: RouteRecordNormalized | null =
    to.matched.find((item) => item.meta.store !== undefined) || null;
  console.log(containStore);

  if (containStore) {
    // @ts-ignore
    containStore.meta.store().load();
  }
});

export default router;
