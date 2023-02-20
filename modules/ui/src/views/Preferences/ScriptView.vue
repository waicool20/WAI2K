<template>
  <main>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Restart enabled</span>
        <input type="checkbox" class="toggle" checked />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Loop delay (s)</span>
        <input
          type="number"
          placeholder="delay (s)"
          step="1"
          min="1"
          max="1000"
          class="input w-1/4 max-w-xs"
        />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Base navigation delay (ms)</span>
        <input
          type="number"
          placeholder="delay (ms)"
          step="1"
          min="1"
          max="10000"
          class="input w-1/4 max-w-xs"
        />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Mouse delay (s)</span>
        <input
          type="number"
          placeholder="delay (s)"
          step="0.01"
          min="0.01"
          max="1000"
          class="input w-1/4 max-w-xs"
        />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Default similarity threshold</span>
        <input
          type="number"
          placeholder="threshold"
          step="0.05"
          min="0.05"
          max="10000"
          class="input w-1/4 max-w-xs"
        />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Map runner threshold</span>
        <input
          type="number"
          placeholder="threshold"
          step="0.05"
          min="0.05"
          max="10000"
          class="input w-1/4 max-w-xs"
        />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">OCR threshold</span>
        <input
          type="number"
          placeholder="threshold"
          step="0.1"
          min="0.1"
          max="10000"
          class="input w-1/4 max-w-xs"
        />
      </label>
    </div>
    <div class="form-control">
      <label class="label cursor-pointer">
        <span class="label-text">Max post battle clicks</span>
        <input
          type="number"
          placeholder="clicks count"
          step="1"
          min="-1"
          max="10000"
          class="input w-1/4 max-w-xs"
        />
      </label>
    </div>
  </main>
</template>
