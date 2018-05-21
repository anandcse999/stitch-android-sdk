/*
 * Copyright 2018-present MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.stitch.android.core.internal;

import com.google.android.gms.tasks.Task;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.core.auth.StitchAuth;
import com.mongodb.stitch.android.core.auth.internal.StitchAuthImpl;
import com.mongodb.stitch.android.core.internal.common.TaskDispatcher;
import com.mongodb.stitch.android.core.services.internal.NamedServiceClientFactory;
import com.mongodb.stitch.android.core.services.internal.ServiceClientFactory;
import com.mongodb.stitch.android.core.services.internal.StitchServiceImpl;
import com.mongodb.stitch.core.StitchAppClientConfiguration;
import com.mongodb.stitch.core.StitchAppClientInfo;
import com.mongodb.stitch.core.internal.CoreStitchAppClient;
import com.mongodb.stitch.core.internal.net.StitchAppRoutes;
import com.mongodb.stitch.core.internal.net.StitchRequestClient;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import org.bson.codecs.Decoder;
import org.bson.codecs.configuration.CodecRegistry;

public final class StitchAppClientImpl implements StitchAppClient {

  private final CoreStitchAppClient coreClient;
  private final TaskDispatcher dispatcher;
  private final StitchAppClientInfo info;
  private final StitchAppRoutes routes;
  private final StitchAuthImpl auth;

  /**
   * Constructs an app client with the given configuration.
   *
   * @param config the configuration to use for the app client.
   */
  public StitchAppClientImpl(final StitchAppClientConfiguration config) {

    this.dispatcher = new TaskDispatcher();
    this.info =
        new StitchAppClientInfo(
            config.getClientAppId(),
            config.getDataDirectory(),
            config.getLocalAppName(),
            config.getLocalAppVersion(),
            config.getCodecRegistry());
    this.routes = new StitchAppRoutes(this.info.getClientAppId());
    final StitchRequestClient requestClient =
        new StitchRequestClient(
                config.getBaseUrl(), config.getTransport(), config.getDefaultRequestTimeout());
    this.auth =
        new StitchAuthImpl(
            requestClient, this.routes.getAuthRoutes(), config.getStorage(), dispatcher, this.info);
    this.coreClient = new CoreStitchAppClient(this.auth, this.routes, config.getCodecRegistry());
  }

  @Override
  public StitchAuth getAuth() {
    return auth;
  }

  @Override
  public <T> T getServiceClient(
      final NamedServiceClientFactory<T> provider, final String serviceName) {
    return provider.getClient(
        new StitchServiceImpl(
            auth, routes.getServiceRoutes(), serviceName, info.getCodecRegistry(), dispatcher),
        info,
        dispatcher);
  }

  @Override
  public <T> T getServiceClient(final ServiceClientFactory<T> provider) {
    return provider.getClient(
        new StitchServiceImpl(
            auth, routes.getServiceRoutes(), "", info.getCodecRegistry(), dispatcher),
        info,
        dispatcher);
  }

  @Override
  public <ResultT> Task<ResultT> callFunction(
      final String name, final List<?> args, final Class<ResultT> resultClass) {
    return dispatcher.dispatchTask(
      new Callable<ResultT>() {
        @Override
        public ResultT call() {
          return coreClient.callFunctionInternal(name, args, null, resultClass);
        }
      });
  }

  @Override
  public <ResultT> Task<ResultT> callFunction(
      final String name,
      final List<?> args,
      final Long requestTimeout,
      final Class<ResultT> resultClass) {
    return dispatcher.dispatchTask(
      new Callable<ResultT>() {
        @Override
        public ResultT call() {
          return coreClient.callFunctionInternal(name, args, requestTimeout, resultClass);
        }
      });
  }

  @Override
  public <ResultT> Task<ResultT> callFunction(
      final String name,
      final List<?> args,
      final Class<ResultT> resultClass,
      final CodecRegistry codecRegistry
  ) {
    return dispatcher.dispatchTask(
      new Callable<ResultT>() {
        @Override
        public ResultT call() {
          return coreClient.callFunctionInternal(name, args, null, resultClass, codecRegistry);
        }
      });
  }

  @Override
  public <ResultT> Task<ResultT> callFunction(
      final String name,
      final List<?> args,
      final Long requestTimeout,
      final Class<ResultT> resultClass,
      final CodecRegistry codecRegistry
  ) {
    return dispatcher.dispatchTask(
      new Callable<ResultT>() {
        @Override
        public ResultT call() {
          return coreClient.callFunctionInternal(
              name,
              args,
              requestTimeout,
              resultClass,
              codecRegistry);
        }
      });
  }

  @Override
  public <ResultT> Task<ResultT> callFunction(
      final String name, final List<?> args, final Decoder<ResultT> resultDecoder) {
    return dispatcher.dispatchTask(
      new Callable<ResultT>() {
        @Override
        public ResultT call() {
          return coreClient.callFunctionInternal(name, args, null, resultDecoder);
        }
      });
  }

  @Override
  public <ResultT> Task<ResultT> callFunction(
      final String name,
      final List<?> args,
      final Long requestTimeout,
      final Decoder<ResultT> resultDecoder) {
    return dispatcher.dispatchTask(
      new Callable<ResultT>() {
        @Override
        public ResultT call() {
          return coreClient.callFunctionInternal(name, args, requestTimeout, resultDecoder);
        }
      });
  }

  /**
   * Closes the client and shuts down all background operations.
   */
  @Override
  public void close() throws IOException {
    auth.close();
    dispatcher.close();
  }
}
