<template>
  <input type="checkbox" id="assignment-modal" class="modal-toggle" />
  <label
    for="assignment-modal"
    class="modal cursor-pointer"
    @click.self="closeModal"
  >
    <label class="modal-box relative w-11/12 max-w-7xl" for="">
      <label
        for="assignment-modal"
        class="btn btn-sm btn-circle absolute right-2 top-2"
        @click.self="closeModal"
        >✕</label
      >
      <p class="text-lg font-bold pb-4 max-w-full">
        Echelon #{{ index }} assignments
      </p>
      <span class="grid grid-cols-8 gap-1">
        <span
          v-for="logisticsMap in maps.logistics"
          :key="logisticsMap.chapterIndex"
        >
          <label class="label cursor-pointer">
            <input
              type="checkbox"
              v-model="assignment"
              :value="logisticsMap.number"
              class="checkbox"
            />
            <span class="label-text">{{ logisticsMap.formattedString }}</span>
          </label>
        </span>
      </span>
      <span class="modal-action">
        <label
          for="assignment-modal"
          class="btn btn-primary"
          @click.self="saveModal"
          >Save</label
        >
      </span>
    </label>
  </label>
</template>

<script setup lang="ts">
import { useClassifierStore } from "@/stores/classifiers";
import { storeToRefs } from "pinia";
import {computed} from "vue";
import type { ComputedRef } from "vue";
import { useProfileStore } from "@/stores/profile";

interface Props {
  index: string;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const props = withDefaults(defineProps<Props>(), {
  index: () => "",
});
const emit = defineEmits(["close", "save"]);

const closeModal = () => {
  emit("close");
};
const saveModal = () => {
  profileStore.setAssignment(props.index, assignment.value);
  emit("save", assignment, props.index);
};

const classifierStore = useClassifierStore();
const profileStore = useProfileStore();

const assignment = computed({
  get() {
    return profileStore.getAssignment(props.index);
  },
  set(value: number[]) {
    if (props.index) {
      profileStore.setAssignment(props.index, value);
    }
  },
});

const { maps } = storeToRefs(classifierStore);
</script>
