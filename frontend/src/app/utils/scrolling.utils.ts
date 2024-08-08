// Inspired by com.google.gwt.dom.client.DOMImpl#scrollIntoView.
function doScrollIntoElement(element: HTMLElement, scrollToCenter: boolean) {
  let left = element.offsetLeft, top = element.offsetTop;
  const width = element.offsetWidth, height = element.offsetHeight;

  if (element.parentElement !== element.offsetParent) {
    left -= element.parentElement!.offsetLeft;
    top -= element.parentElement!.offsetTop;
  }

  let currentNode = element.parentElement;
  while (currentNode && (currentNode.nodeType === 1)) {
    if (left < currentNode.scrollLeft) {
      if (scrollToCenter) {
        currentNode.scrollLeft = left - (currentNode.clientWidth / 2 - width * 0.5);
      } else {
        currentNode.scrollLeft = left;
      }
    } else {
      if (scrollToCenter) {
        currentNode.scrollLeft = (left + (currentNode.clientWidth / 2 + width * 0.5)) - currentNode.clientWidth;
      } else if (left + width > currentNode.scrollLeft + currentNode.clientWidth) {
        currentNode.scrollLeft = (left + width) - currentNode.clientWidth;
      }
    }
    if (top < currentNode.scrollTop) {
      if (scrollToCenter) {
        currentNode.scrollTop = top - (currentNode.clientHeight / 2 - height * 0.5);
      } else {
        currentNode.scrollTop = top;
      }
    } else {
      if (scrollToCenter) {
        currentNode.scrollTop = (top + (currentNode.clientHeight / 2 + height * 0.5)) - currentNode.clientHeight;
      } else if (top + height > currentNode.scrollTop + currentNode.clientHeight) {
        currentNode.scrollTop = (top + height) - currentNode.clientHeight;
      }
    }

    let offsetLeft = currentNode.offsetLeft, offsetTop = currentNode.offsetTop;
    if (currentNode.parentElement !== currentNode.offsetParent) {
      offsetLeft -= currentNode.parentElement!.offsetLeft;
      offsetTop -= currentNode.parentElement!.offsetTop;
    }

    left += offsetLeft - currentNode.scrollLeft;
    top += offsetTop - currentNode.scrollTop;
    currentNode = currentNode.parentElement;
  }
}

export namespace ScrollingUtils {
  export function scrollIntoElement(element: HTMLElement, scrollToCenter = true) {
    // Make sure element is properly positioned before scrolling.
    window.requestAnimationFrame(() => doScrollIntoElement(element, scrollToCenter));
  }
}
