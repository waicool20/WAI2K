<template>
  <nav class="navbar bg-primary text-primary-content rounded-md mb-8 shadow-xl">
    <div class="navbar-start">
      <RouterLink
        :to="{ name: 'welcome' }"
        class="btn btn-ghost h-12 m-0 ml-0 pl-0"
      >
        <div class="avatar rounded-full overflow-clip h-12 w-12 mr-2">
          <img src="@/assets/logo.png" alt="WAI2K" class="mx-auto" />
        </div>
        WAI2K Dash
      </RouterLink>
    </div>
    <div class="navbar-center">

      <ul class="menu menu-horizontal p-0">
        <li>
          <RouterLink
              :to="{ name: 'welcome' }"
              active-class="active rounded-md"
              class="hover:rounded-md focus:rounded-md"
          >
            <font-awesome-icon icon="fa-solid fa-home" />
            Welcome
          </RouterLink>
        </li>
        <li>
          <RouterLink
              :to="{ name: 'status' }"
              active-class="active rounded-md"
              class="hover:rounded-md focus:rounded-md"
          >
            <font-awesome-icon icon="fa-solid fa-circle-info" />
            Status
          </RouterLink>
        </li>
        <li>
          <RouterLink
              :to="{ name: 'logistics' }"
              active-class="active rounded-md"
              class="hover:rounded-md focus:rounded-md"
          >
            <font-awesome-icon icon="fa-solid fa-user" />
            Profile
          </RouterLink>
        </li>
        <li>
          <RouterLink
              :to="{ name: 'restart' }"
              active-class="active rounded-md"
              class="hover:rounded-md focus:rounded-md"
          >
            <font-awesome-icon icon="fa-solid fa-wrench" />
            Preferences
          </RouterLink>
        </li>
        <li>
          <RouterLink
              :to="{ name: 'about' }"
              icon="fa-solid fa-circle-question"
              active-class="active rounded-md"
              class="hover:rounded-md focus:rounded-md"
          >
            <font-awesome-icon icon="fa-solid fa-circle-question" />
            About
          </RouterLink>
        </li>
      </ul>
    </div>
    <div class="navbar-end">
      <ProfileSelector class="dropdown dropdown-left" />
    </div>
  </nav>
</template>
<script setup lang="ts">
import ProfileSelector from "@/components/ProfileSelector.vue";
</script>
