<template>
  <main class="container pb-24">
    <div>
      <LayoutHeader class="mb-4" />
      <RouterView class="mb-4" />
      <LayoutFooter />
    </div>
    <FooterNav />
    <div id="modals">
      <ConsoleView />
    </div>
    <div id="changes"></div>
  </main>
</template>

<script setup lang="ts">
import { RouterView } from "vue-router";
import LayoutHeader from "./components/LayoutHeader.vue";
import LayoutFooter from "./components/LayoutFooter.vue";
import FooterNav from "@/components/FooterNav.vue";
import ConsoleView from "@/components/ConsoleView.vue";
import { useClassifierStore } from "@/stores/classifiers";
import { onBeforeMount } from "vue";
import { useConfigStore } from "@/stores/config";
import { useProfileStore } from "@/stores/profile";
import { useProfileListStore } from "@/stores/profileList";

const classifierStore = useClassifierStore();
const configStore = useConfigStore();
const profileStore = useProfileStore();
const profileListStore = useProfileListStore();

configStore.load().then(async () => {
  await Promise.all([
    classifierStore.load(),
    profileListStore.load(),
    profileStore.load(configStore.current_profile),
  ]);
});
console.log(configStore.current_profile);
</script>
