<template>
  <div class="grid grid-cols-4">
    <div class="card card-side">
      <figure class="w-8">
        <img src="@/assets/cano.webp" alt=":heh:" />
      </figure>
      <div class="card-body pt-0 pb-0 pl-2 pr-0">
        <h2 class="card-title">is a meme!</h2>
      </div>
    </div>

    <div class="card">
      <div class="card-body pt-0 pb-0 pl-2 pr-0">
        <h2 class="card-title">
          <a href="#" @click.prevent="saveConfig()">save config</a>
        </h2>
      </div>
    </div>

    <div class="card">
      <div class="card-body pt-0 pb-0 pl-2 pr-0">
        <h2 class="card-title">
          <a href="#" @click.prevent="saveProfile()"
            >save profile {{ profileName }}</a
          >
        </h2>
      </div>
    </div>
  </div>
</template>
<script setup lang="ts">
import { useConfigStore } from "@/stores/config";
import { useProfileStore } from "@/stores/profile";
import { computed } from "vue";

const profileName = computed(() => {
  return useConfigStore().current_profile;
});
const saveConfig = () => {
  useConfigStore().save();
};

const saveProfile = () => {
  useProfileStore().save(useConfigStore().current_profile);
};
</script>
