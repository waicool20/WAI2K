<template>
  <div>
    <label tabindex="0" class="btn btn-ghost rounded-btn m-1">{{
      profileName
    }}</label>
    <ul
      tabindex="0"
      class="dropdown-content menu p-2 shadow-xl bg-base-100 rounded-box w-100"
    >
      <li
        v-for="(profile, index) in store.profiles"
        :key="index"
        class="grid grid-cols-5 m-2 gap-2"
      >
        <a
          @click.prevent="loadProfile(profile.toString())"
          class="col-span-4"
          :class="{ active: profile === profileName }"
          >{{ profile }}</a
        >
        <a
          class="btn btn-error"
          @click.prevent="deleteProfile(profile.toString())"
        >
          <font-awesome-icon icon="fa-solid fa-trash" />
        </a>
      </li>
      <li class="cursor-default">
        <div
          class="divider hover:bg-transparent focus:bg-transparent cursor-default"
        >
          Add new
        </div>
      </li>
      <li class="hover:bg-transparent focus:bg-transparent mx-2">
        <div
          class="form-control hover:bg-transparent focus:bg-transparent px-0"
        >
          <div class="input-group">
            <input
              type="text"
              ref="newProfileInput"
              placeholder="Profile name"
              class="input"
            />
            <button class="btn btn-square" @click="createProfile()">
              <font-awesome-icon icon="fa-solid fa-plus" />
            </button>
          </div>
        </div>
      </li>
    </ul>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { useProfileStore } from "@/stores/profile";
import { useProfileListStore } from "@/stores/profileList";
import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";
import { useConfigStore } from "@/stores/config";

const store = useProfileListStore();
const profileStore = useProfileStore();
const config = useConfigStore();

const newProfileInput = ref(null);

const profileName = computed({
  get: () => {
    return config.current_profile;
  },
  set: (value: string) => {
    config.setCurrentProfile(value);
  },
});

const loadProfile = async (name: string) => {
  config.setCurrentProfile(name);
  await config.save();
  await profileStore.load(name);
};
const deleteProfile = async (name: string) => {
  await store.deleteByName(name);
  await store.load();
  config.setCurrentProfile(store.profiles[0].toString());
};

const createProfile = () => {
  // @ts-ignore
  let profileNameInput = newProfileInput.value.value;
  profileStore.save(profileNameInput).then(() => {
    config.load().then(() => {
      store.load();
      profileNameInput = "";
    });
  });
};

onMounted(() => {
  config.load().then(async () => {
    await Promise.all([store.load(), profileStore.load(profileName.value)]);
  });
});
</script>
