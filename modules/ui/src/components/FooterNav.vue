<template>
  <footer
    class="w-auto h-16 bg-primary shadow-2xl border-white w-full fixed left-0 bottom-0 flex gap-2 justify-center items-center text-white"
  >
    <label class="btn" for="terminal-modal">
      <font-awesome-icon icon="fa-solid fa-terminal" />
    </label>
    <ProfileSelector class="" />
    <button class="btn">
      <font-awesome-icon icon="fa-solid fa-floppy-disk" />
    </button>
    <button class="btn">
      <font-awesome-icon icon="fa-solid fa-rotate" />
    </button>
    <button class="btn btn-warning">
      <font-awesome-icon icon="fa-solid fa-rotate" />
    </button>
  </footer>
</template>

<script setup lang="ts">
import ProfileSelector from "@/components/ProfileSelector.vue";
import { useAppStore } from "@/stores/app";

const appStore = useAppStore();
</script>
<style scoped></style>
