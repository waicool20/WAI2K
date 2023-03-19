<script setup lang="ts">
import { RouterView } from "vue-router";
import LayoutHeader from "./components/LayoutHeader.vue";
import LayoutFooter from "./components/LayoutFooter.vue";

import { useClassifierStore } from "@/stores/classifiers";

const store = useClassifierStore();
store.load();
</script>

<template>
  <main>
    <div class="flex flex-wrap">
      <div class="basis-full">
        <LayoutHeader />
      </div>
      <RouterView />
      <div class="basis-full">
        <LayoutFooter />
      </div>
    </div>
    <div id="modals"></div>
    <div id="changes"></div>
  </main>
</template>
