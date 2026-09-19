async (page) => {
  const frame = page.frames().find((candidate) => candidate !== page.mainFrame());
  if (!frame) throw new Error('Coze iframe not found');

  await page.keyboard.press('Escape');
  await frame.getByRole('button', { name: '确定', exact: true }).click();
  await frame.waitForTimeout(1800);

  let copied = false;
  let method = '';
  const copySelectors = [
    '[aria-label*="复制"]',
    '[title*="复制"]',
    'button:has-text("复制")',
  ];

  for (const selector of copySelectors) {
    const items = frame.locator(selector);
    for (let index = (await items.count()) - 1; index >= 0; index -= 1) {
      const item = items.nth(index);
      if (await item.isVisible().catch(() => false)) {
        await item.click();
        copied = true;
        method = 'copy-control';
        break;
      }
    }
    if (copied) break;
  }

  let tokenFieldCount = 0;
  if (!copied) {
    const fields = frame.locator('input, textarea');
    for (let index = (await fields.count()) - 1; index >= 0; index -= 1) {
      const field = fields.nth(index);
      if (!(await field.isVisible().catch(() => false))) continue;
      const value = await field.inputValue().catch(() => '');
      const tokenLike = /^(?:pat[_-]?[A-Za-z0-9_-]*|[A-Za-z0-9_-]{50,})$/;
      if (value.length >= 40 && tokenLike.test(value)) {
        tokenFieldCount += 1;
        await field.click();
        await field.press('Control+A');
        await field.press('Control+C');
        copied = true;
        method = 'token-field';
        break;
      }
    }
  }

  const createdNameVisible = await frame
    .getByText('AI备忘录运行-20260905', { exact: true })
    .last()
    .isVisible()
    .catch(() => false);

  return {
    submitClicked: true,
    copied,
    method,
    tokenFieldCount,
    createdNameVisible,
  };
}
