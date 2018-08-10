package com.xiongxin.sample.memcache;

import io.vertx.core.Future;

import java.util.List;

public interface RawMemcache {
  Future<List<GetResult>> get(List<GetOperation> batch);
}
