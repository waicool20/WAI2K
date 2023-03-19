<template>
  <h2 class="text-xl my-4 px-1">Auto battle</h2>
  <div class="form-control">
    <label class="label cursor-pointer">
      <span class="label-text">Enabled</span>
      <input
        type="checkbox"
        class="toggle"
        v-model="enabled"
        :true-value="true"
        :false-value="false"
      />
    </label>
  </div>
  <p class="my-4 px-1">
    NOTE: Auto battle echelons should always be in a higher echelon number than
    logistic echelons so they don't interfere with status reading
  </p>
  <p class="my-4 px-1">
    NOTE: Disabling this will lead to restarts when the app runs into an auto
    battle echelon
  </p>
</template>
<script setup lang="ts">
import { useProfileStore } from "@/stores/profile";
import { computed } from "vue";

const store = useProfileStore();

const enabled = computed({
  get() {
    return store.autoBattleEnabled;
  },
  set(value) {
    store.setAutoBattleEnabled(value);
  },
});
</script>
