async (page) => {
  const frame = page.frames().find((candidate) => candidate !== page.mainFrame());
  if (!frame) throw new Error('Coze iframe not found');

  const outcome = await frame.locator('body').evaluate((body) => {
    const tokenPattern = /^pat[_-]?[A-Za-z0-9._-]{30,}$/;
    const possibleValues = [];

    for (const element of body.querySelectorAll('input, textarea')) {
      possibleValues.push(element.value || '');
    }

    for (const element of body.querySelectorAll('*')) {
      if (element.children.length === 0) {
        possibleValues.push(element.textContent || '');
      }
      for (const attribute of ['data-clipboard-text', 'data-value', 'value']) {
        possibleValues.push(element.getAttribute(attribute) || '');
      }
    }

    const token = possibleValues
      .map((value) => value.trim())
      .find((value) => tokenPattern.test(value));

    if (!token) return { found: false, copied: false, length: 0 };

    const textarea = document.createElement('textarea');
    textarea.value = token;
    textarea.setAttribute('readonly', '');
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    body.appendChild(textarea);
    textarea.select();
    const copied = document.execCommand('copy');
    textarea.remove();

    return { found: true, copied, length: token.length };
  });

  return outcome;
}
