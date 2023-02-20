<template>
  <div class="flex flex-col lg:flex-row basis-full gap-4 lg:gap-8">
    <div class="basis-1/4 shadow-xl mb-8 p-8 bg-neutral rounded-md">
      <ul
        class="menu w-full menu-horizontal lg:menu-vertical"
        v-if="childrenRoutes.length > 0"
      >
        <li
          v-for="link in childrenRoutes"
          :key="link.name"
          class="mx-2 lg:mx-0"
        >
          <RouterLink
            :to="{ name: link.name }"
            active-class="active rounded-md"
            class="hover:rounded-md focus:rounded-md"
          >
            {{ link.meta?.title }}
          </RouterLink>
        </li>
      </ul>
    </div>
    <div class="basis-3/4 shadow-xl mb-8 p-8 bg-neutral rounded-md">
      <router-view></router-view>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, defineProps, reactive } from "vue";
import { useRouter } from "vue-router";
import type { Router, RouteRecordRaw } from "vue-router";

interface Props {
  route: String;
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const props = defineProps<Props>();

const router: Router = useRouter();

const childrenRoutes = computed(() => {
  let routes: RouteRecordRaw[] = [];
  for (const routeRecord of router.options.routes) {
    if (
      // @ts-ignore
      routeRecord.name === props.route &&
      typeof routeRecord.children !== "undefined" &&
      routeRecord.children.length > 0
    ) {
      routeRecord.children.map((item) => {
        routes.push(item);
      });
      routes.push();
    }
  }

  return reactive(routes);
});
</script>
