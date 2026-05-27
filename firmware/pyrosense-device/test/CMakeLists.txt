cmake_minimum_required(VERSION 3.16)
project(pyrosense-device-tests CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Host build flag — disables ESP-IDF dependencies
add_definitions(-DPYRO_HOST_BUILD)

# Source directories
set(MAIN_DIR ${CMAKE_SOURCE_DIR}/../main)

# Collect sources (exclude app_main to avoid duplicate main)
set(LIB_SOURCES
    ${MAIN_DIR}/config/device_config.cpp
    ${MAIN_DIR}/config/state_machine.cpp
    ${MAIN_DIR}/sensors/sensor_registry.cpp
    ${MAIN_DIR}/sensors/mock_sensor_provider.cpp
    ${MAIN_DIR}/signal_processing/feature_extractor.cpp
    ${MAIN_DIR}/signal_processing/rms_calculator.cpp
    ${MAIN_DIR}/signal_processing/signal_quality.cpp
    ${MAIN_DIR}/telemetry/payload_builder.cpp
    ${MAIN_DIR}/telemetry/heartbeat_builder.cpp
    ${MAIN_DIR}/connectivity/mqtt_client.cpp
    ${MAIN_DIR}/connectivity/wifi_manager.cpp
    ${MAIN_DIR}/security/hmac_signer.cpp
    ${MAIN_DIR}/security/nonce_generator.cpp
    ${MAIN_DIR}/storage/offline_queue.cpp
    ${MAIN_DIR}/diagnostics/self_test.cpp
    ${MAIN_DIR}/diagnostics/logger.cpp
    ${MAIN_DIR}/utils/time_utils.cpp
    ${MAIN_DIR}/utils/uuid_utils.cpp
)

# Library target (reusable for tests)
add_library(pyrosense_lib STATIC ${LIB_SOURCES})
target_include_directories(pyrosense_lib PUBLIC ${MAIN_DIR})

# --- Unit Tests ---
add_executable(test_rms unit/test_rms.cpp)
target_link_libraries(test_rms pyrosense_lib)

add_executable(test_signal_quality unit/test_signal_quality.cpp)
target_link_libraries(test_signal_quality pyrosense_lib)

add_executable(test_state_machine unit/test_state_machine.cpp)
target_link_libraries(test_state_machine pyrosense_lib)

add_executable(test_offline_queue unit/test_offline_queue.cpp)
target_link_libraries(test_offline_queue pyrosense_lib)

add_executable(test_payload unit/test_payload.cpp)
target_link_libraries(test_payload pyrosense_lib)

add_executable(test_nonce unit/test_nonce.cpp)
target_link_libraries(test_nonce pyrosense_lib)

# --- Integration test (full pipeline) ---
add_executable(test_integration_pipeline unit/test_integration_pipeline.cpp)
target_link_libraries(test_integration_pipeline pyrosense_lib)

# --- CTest registration ---
enable_testing()
add_test(NAME RmsCalculator COMMAND test_rms)
add_test(NAME SignalQuality COMMAND test_signal_quality)
add_test(NAME StateMachine COMMAND test_state_machine)
add_test(NAME OfflineQueue COMMAND test_offline_queue)
add_test(NAME PayloadBuilder COMMAND test_payload)
add_test(NAME NonceGenerator COMMAND test_nonce)
add_test(NAME IntegrationPipeline COMMAND test_integration_pipeline)
